using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// Thin HTTP client for the GraphWalker REST service (port 8887).
/// Used by each model runner to ask "what is the next step?".
/// </summary>
public class GraphWalkerClient
{
    private readonly HttpClient _http;
    private readonly string _baseUrl;

    public GraphWalkerClient(string baseUrl = "http://localhost:8887")
    {
        _baseUrl = baseUrl;
        _http    = new HttpClient { Timeout = TimeSpan.FromSeconds(10) };
    }

    // ── Session lifecycle ────────────────────────────────────────────────────
    public void Start()
    {
        var resp = _http.PutAsync($"{_baseUrl}/graphwalker/start", null).Result;
        EnsureSuccess(resp, "start");
    }

    public bool HasNext()
    {
        var resp = _http.GetAsync($"{_baseUrl}/graphwalker/hasNext").Result;
        EnsureSuccess(resp, "hasNext");
        var body = resp.Content.ReadAsStringAsync().Result;
        var obj  = JObject.Parse(body);
        return obj["hasNext"]?.ToString() == "true";
    }

    public (string elementId, string elementName) GetNext()
    {
        var resp = _http.GetAsync($"{_baseUrl}/graphwalker/getNext").Result;
        EnsureSuccess(resp, "getNext");
        var body = resp.Content.ReadAsStringAsync().Result;
        var obj  = JObject.Parse(body);
        return (
            obj["currentElementId"]?.ToString() ?? string.Empty,
            obj["currentElementName"]?.ToString() ?? string.Empty
        );
    }

    public bool IsFulfilled()
    {
        var resp = _http.GetAsync($"{_baseUrl}/graphwalker/fulfilled").Result;
        EnsureSuccess(resp, "fulfilled");
        var body = resp.Content.ReadAsStringAsync().Result;
        var obj  = JObject.Parse(body);
        return obj["fulfilled"]?.ToString() == "true";
    }

    // ── Data / statistics ────────────────────────────────────────────────────
    public JObject GetStatistics()
    {
        var resp = _http.GetAsync($"{_baseUrl}/graphwalker/getStatistics").Result;
        EnsureSuccess(resp, "getStatistics");
        return JObject.Parse(resp.Content.ReadAsStringAsync().Result);
    }

    // ── Private helpers ──────────────────────────────────────────────────────
    private static void EnsureSuccess(HttpResponseMessage resp, string action)
    {
        if (!resp.IsSuccessStatusCode)
        {
            var body = resp.Content.ReadAsStringAsync().Result;
            throw new Exception($"GraphWalker [{action}] failed {(int)resp.StatusCode}: {body}");
        }
    }
}
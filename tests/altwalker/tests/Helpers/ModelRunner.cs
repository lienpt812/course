using System.Reflection;
using NUnit.Framework;

namespace CourseRegistration.Tests.Helpers;

/// <summary>
/// Generic runner: iterates GraphWalker steps and calls the matching method
/// on the model instance by reflection.
/// </summary>
public static class ModelRunner
{
    /// <summary>
    /// Run a full GraphWalker walk on <paramref name="model"/>.
    /// GraphWalker REST service must already be running with the correct JSON loaded.
    /// </summary>
    /// <param name="model">Model instance with v_* and e_* methods.</param>
    /// <param name="gwClient">Client connected to the running GW service.</param>
    /// <param name="maxSteps">Safety ceiling to prevent infinite loops.</param>
    public static void Run(object model, GraphWalkerClient gwClient, int maxSteps = 500)
    {
        var modelType  = model.GetType();
        var allMethods = modelType
            .GetMethods(BindingFlags.Public | BindingFlags.Instance)
            .ToDictionary(m => m.Name, StringComparer.OrdinalIgnoreCase);

        // Call setUpModel if present
        if (allMethods.TryGetValue("setUpModel", out var setup))
            setup.Invoke(model, null);

        gwClient.Start();

        int steps = 0;
        while (gwClient.HasNext() && steps < maxSteps)
        {
            steps++;
            var (_, elementName) = gwClient.GetNext();

            if (string.IsNullOrWhiteSpace(elementName)) continue;

            if (allMethods.TryGetValue(elementName, out var method))
            {
                try
                {
                    method.Invoke(model, null);
                }
                catch (TargetInvocationException tie)
                {
                    // Unwrap so NUnit sees the real assertion failure
                    throw tie.InnerException ?? tie;
                }
            }
            else
            {
                TestContext.Progress.WriteLine($"[SKIP] No method for step: {elementName}");
            }
        }

        if (steps >= maxSteps)
            Assert.Fail($"ModelRunner hit maxSteps={maxSteps}. Possible infinite loop in model.");

        TestContext.Progress.WriteLine($"[DONE] {modelType.Name} — {steps} steps completed.");
        var stats = gwClient.GetStatistics();
        TestContext.Progress.WriteLine($"[STATS] {stats}");
    }
}
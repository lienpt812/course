package com.eduplatform.mbt.runners;

import com.eduplatform.mbt.support.GraphWalkerExecutionPolicy;
import com.eduplatform.mbt.support.SelenideSetup;
import com.eduplatform.mbt.support.TestContext;
import com.eduplatform.mbt.support.TestDataSeeder;
import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.GraphWalker;
import org.graphwalker.java.annotation.Model;
import org.graphwalker.java.test.Result;
import org.graphwalker.java.test.TestBuilder;
import org.graphwalker.java.test.TestExecutionException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base runner. Subclasses provide the impl class annotated with
 * {@code @GraphWalker(value=..., start=...)} + implementing the generated model
 * interface carrying {@code @Model(file="...json")}.
 *
 * <p>We load the model file from the classpath, then use
 * {@code TestBuilder.addContext(Context, Path, String)} which:
 * <ol>
 *   <li>parses the JSON model into a RuntimeModel</li>
 *   <li>assigns model + next element (from the JSON's {@code startElementId})</li>
 *   <li>parses the path-generator string (e.g. bounded
 *   {@code random(edge_coverage(100) || length(50))} — see {@link GraphWalkerExecutionPolicy})</li>
 * </ol>
 * This avoids the "A context must be associated with a model" machine error.</p>
 *
 * <p><strong>Setup order:</strong> JUnit 5 can invoke a {@code @BeforeAll} on this superclass
 * <em>before</em> the concrete test class (e.g. {@code AuthRunner}) is initialized, so
 * {@code static \{ \}} in the runner would not run. Each concrete runner must call
 * {@link #initSuiteBeforeModel()} from its <em>own</em> {@code @BeforeAll} (after any
 * {@code System.setProperty} for seeding, if needed).</p>
 */
public abstract class AbstractGraphWalkerRunner {

    private static final Logger log = LoggerFactory.getLogger(AbstractGraphWalkerRunner.class);

    /**
     * Selenide + one-shot API seed. Call from a {@code @BeforeAll} on the <strong>concrete</strong>
     * runner class, not only from a {@code @BeforeAll} declared here, so per-runner static init runs first.
     */
    public static void initSuiteBeforeModel() {
        SelenideSetup.init();
        TestDataSeeder.seedOnce();
    }

    @AfterAll
    static void tearDown() {
        SelenideSetup.tearDown();
    }

    protected abstract Class<? extends ExecutionContext> contextClass();

    protected void runModel() {
        runModelFor(contextClass());
    }

    /**
     * Run any execution context (e.g. a journey subclass) without changing the original runner’s
     * {@link #contextClass()}.
     */
    public static void runModelFor(Class<? extends ExecutionContext> implClass) {
        if (implClass == null) {
            throw new IllegalArgumentException("implClass is null");
        }
        log.info("Starting GraphWalker run for {}", implClass.getSimpleName());
        TestContext.resetAuthStateForThread();

        ExecutionContext impl = instantiate(implClass);
        Path modelPath = resolveModelPath(implClass);
        String generator = resolveGenerator(implClass);
        log.info("Model file: {}, generator: {}", modelPath, generator);

        Result result;
        try {
            result = new TestBuilder()
                    .addContext(impl, modelPath, generator)
                    .execute();
        } catch (TestExecutionException ex) {
            Result partial = ex.getResult();
            if (partial != null) {
                recordStatistics(partial, implClass);
            }
            throw new RuntimeException("Failed to execute GraphWalker for " + implClass.getSimpleName(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute GraphWalker for " + implClass.getSimpleName(), ex);
        }

        log.info("GraphWalker finished: errors={}", result.hasErrors() ? "YES" : "no");
        if (result.hasErrors()) {
            log.error("GraphWalker errors:\n{}", String.join("\n", result.getErrors()));
        }
        recordStatistics(result, implClass);
        assertThat(result.hasErrors())
                .as("GraphWalker run must finish without errors")
                .isFalse();
    }

    /**
     * Logs edge/vertex coverage and (optionally) writes JSON to {@code target/graphwalker-reports/}.
     * Disable file output: {@code -Dgraphwalker.statistics.file=false}.
     */
    private static void recordStatistics(Result result, Class<? extends ExecutionContext> cls) {
        JSONObject j = result.getResults();
        if (j == null) {
            log.warn("GraphWalker Result has no statistics (getResults() is null)");
            return;
        }

        String summary = MessageFormat.format(
                "edgeCoverage={0}% vertexCoverage={1}% edges={2}/{3}({4} unvisited) vertices={5}/{6}({7} unvisited)",
                j.optInt("edgeCoverage", -1),
                j.optInt("vertexCoverage", -1),
                j.optInt("totalNumberOfVisitedEdges", -1),
                j.optInt("totalNumberOfEdges", -1),
                j.optInt("totalNumberOfUnvisitedEdges", -1),
                j.optInt("totalNumberOfVisitedVertices", -1),
                j.optInt("totalNumberOfVertices", -1),
                j.optInt("totalNumberOfUnvisitedVertices", -1));
        log.info("GraphWalker statistics [{}]: {}", cls.getSimpleName(), summary);

        if (log.isDebugEnabled()) {
            log.debug("GraphWalker statistics JSON:\n{}", result.getResultsAsString());
        }

        if (!statisticsFileEnabled()) {
            return;
        }
        Path out = statisticsReportPath(cls);
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, result.getResultsAsString());
            log.info("GraphWalker statistics file: {}", out.toAbsolutePath());
        } catch (Exception ex) {
            log.warn("Could not write GraphWalker statistics file: {}", ex.getMessage());
        }
    }

    private static boolean statisticsFileEnabled() {
        String p = System.getProperty("graphwalker.statistics.file");
        if (p != null && !p.isBlank()) {
            return !"false".equalsIgnoreCase(p.trim());
        }
        return true;
    }

    private static Path statisticsReportPath(Class<? extends ExecutionContext> cls) {
        String dir = System.getProperty("graphwalker.report.dir", "target/graphwalker-reports");
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
        String name = cls.getSimpleName() + "-" + ts + ".json";
        return Paths.get(dir).resolve(name);
    }

    // ---------- helpers ----------
    private static ExecutionContext instantiate(Class<? extends ExecutionContext> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("Cannot instantiate " + cls.getName(), ex);
        }
    }

    private static Path resolveModelPath(Class<? extends ExecutionContext> cls) {
        String modelFile = null;
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Class<?> iface : c.getInterfaces()) {
                Model m = iface.getAnnotation(Model.class);
                if (m != null) {
                    modelFile = m.file();
                    break;
                }
            }
            if (modelFile != null) {
                break;
            }
            Model m = c.getAnnotation(Model.class);
            if (m != null) {
                modelFile = m.file();
                break;
            }
        }
        if (modelFile == null) {
            throw new IllegalStateException("No @Model annotation found on " + cls.getName()
                    + " (or superclasses) / their interfaces");
        }

        ClassLoader cl = cls.getClassLoader() != null ? cls.getClassLoader() : Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(modelFile);
        if (url == null) {
            throw new IllegalStateException("Model resource not on classpath: " + modelFile);
        }
        try {
            return Paths.get(url.toURI());
        } catch (Exception ex) {
            throw new RuntimeException("Cannot resolve model URL to Path: " + url, ex);
        }
    }

    private static String resolveGenerator(Class<? extends ExecutionContext> cls) {
        String override = generatorOverride();
        if (override != null) {
            log.info("Using graphwalker.generator override: {}", override);
            return override;
        }
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            GraphWalker gw = c.getAnnotation(GraphWalker.class);
            if (gw != null) {
                String generator = gw.value();
                if (generator == null || generator.isBlank()) {
                    return GraphWalkerExecutionPolicy.BOUNDED_DEFAULT;
                }
                return generator;
            }
        }
        throw new IllegalStateException("No @GraphWalker annotation on " + cls.getName() + " (or superclasses)");
    }

    /**
     * Same impl, different stop conditions without recompiling:
     * {@code -Dgraphwalker.generator=random(edge_coverage(100))} or env {@code GRAPHWALKER_GENERATOR}.
     * <p>{@code a_star(...)} only accepts {@code reached_vertex(name)} or {@code reached_edge(name)},
     * not {@code edge_coverage} — that combination causes ClassCastException at parse time.
     */
    private static String generatorOverride() {
        String p = System.getProperty("graphwalker.generator");
        if (p != null && !p.isBlank()) {
            return p.trim();
        }
        String e = System.getenv("GRAPHWALKER_GENERATOR");
        if (e != null && !e.isBlank()) {
            return e.trim();
        }
        return null;
    }
}

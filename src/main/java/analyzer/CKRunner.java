package analyzer;

import model.MethodMetrics;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKNotifier;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;

public class CKRunner {
    public static List<MethodMetrics> run(String project, String release, File releaseDir) {
        String projectPath = releaseDir.getAbsolutePath();
        List<MethodMetrics> result = new ArrayList<>();

        new CK().calculate(projectPath, new CKNotifier() {

            @Override
            public void notify(CKClassResult ckClassResult) {
                String filePath = ckClassResult.getFile().replace("\\", "/");
                String basePath = releaseDir.getAbsolutePath().replace("\\", "/");

                // rimuove la parte iniziale fino a release-X.Y.Z/
                if (filePath.startsWith(basePath)) {
                    filePath = filePath.substring(basePath.length() + 1); // salta anche lo slash
                }

                for (CKMethodResult method : ckClassResult.getMethods()) {
                    MethodMetrics m = buildMethodMetrics(project, release, filePath, method);
                    result.add(m);
                }

            }

            @Override
            public void notifyError(@NotNull String sourceFilePath, @NotNull Exception e) {
                System.err.println("Errore su file: " + sourceFilePath + ", " + e.getMessage());
            }

            public void complete() {
                System.out.println("Analisi CK completata.");
            }
        });

        return result;
    }


    private static MethodMetrics buildMethodMetrics(String project, String release, String filePath, CKMethodResult method) {
        MethodMetrics m = new MethodMetrics();
        m.setProject(project);
        m.setReleaseId(release);

        String cleanedMethodName = method.getMethodName().split("/")[0];
        m.setMethodPath(filePath + "/" + cleanedMethodName);

        m.setLoc(method.getLoc());
        // m.setCyclomaticComplexity(method.getCyclo());
        // m.setCognitiveComplexity(method.getCognitiveComplexity());
        // m.setStatements(method.getStatements());
        m.setNestingDepth(method.getMaxNestedBlocks());
        m.setParameterCount(method.getParametersQty());
        m.setCodeSmells(0);

        return m;
    }

}


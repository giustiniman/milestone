package utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import model.Method;
import model.Ticket;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TicketRetriever {

    public List<Method> extractModifiedMethodsFromCommits(List<Ticket> jiraTickets, File projectDir) throws IOException, GitAPIException {
        List<Method> buggyMethods = new ArrayList<>();

        try (Git git = Git.open(projectDir)) {

            Repository repository = git.getRepository();

            for (Ticket ticket : jiraTickets) {
                String issueKey = ticket.getTicketKey(); // es: "BOOKKEEPER-123"

                Iterable<RevCommit> commits = git.log().all().call();
                for (RevCommit commit : commits) {
                    String commitMessage = commit.getFullMessage();
                    if (commitMessage.toUpperCase().contains(issueKey.toUpperCase())) {

                        // Prendi il commit genitore (se esiste)
                        RevCommit parent = null;
                        if (commit.getParentCount() > 0) {
                            parent = commit.getParent(0);
                        }

                        // Differenze tra commit e genitore
                        AbstractTreeIterator oldTreeParser = prepareTreeParser(repository, parent);
                        AbstractTreeIterator newTreeParser = prepareTreeParser(repository, commit);

                        List<DiffEntry> diffs = git.diff()
                                .setOldTree(oldTreeParser)
                                .setNewTree(newTreeParser)
                                .call();

                        for (DiffEntry diff : diffs) {
                            String newPath = diff.getNewPath();
                            if (newPath.endsWith(".java")) {
                                String sourceCode = readFileFromGit(repository, commit, newPath);
                                System.out.println(sourceCode);

                                List<Method> methods = extractMethodsFromSource(sourceCode, newPath);
                                buggyMethods.addAll(methods);
                            }
                        }
                    }
                }
            }
        }

        return buggyMethods;
    }

    public AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        final CanonicalTreeParser parser = new CanonicalTreeParser();
        try (ObjectReader reader = repository.newObjectReader()) {
            if (commit == null) {
                return new EmptyTreeIterator(); // primo commit
            }
            RevTree tree = commit.getTree();
            parser.reset(reader, tree.getId());
        }
        return parser;
    }




    public String readFileFromGit(Repository repository, RevCommit commit, String path) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, commit.getTree())) {
            if (treeWalk != null) {
                ObjectId blobId = treeWalk.getObjectId(0);
                try (ObjectReader reader = repository.newObjectReader()) {
                    byte[] bytes = reader.open(blobId).getBytes();
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }


    public List<Method> extractMethodsFromSource(String sourceCode, String filePath) {
        List<Method> methods = new ArrayList<>();

        CompilationUnit cu;
        try {
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> result = parser.parse(sourceCode);

            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                return methods;
            }
            cu = result.getResult().get();
        } catch (Exception e) {
            return methods; // Errore di parsing
        }

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();

            classDecl.findAll(MethodDeclaration.class).forEach(md -> {
                Method method = new Method();

                method.setMethodName(md.getNameAsString());
                method.setClassName(className);

                // Linee
                method.setStartLine(md.getBegin().map(p -> p.line).orElse(-1));
                method.setEndLine(md.getEnd().map(p -> p.line).orElse(-1));

                // Firma (nome + tipi parametri)
                StringBuilder signatureBuilder = new StringBuilder(md.getNameAsString()).append("(");
                List<String> paramTypes = md.getParameters().stream()
                        .map(p -> p.getType().toString())
                        .collect(Collectors.toList());
                signatureBuilder.append(String.join(",", paramTypes)).append(")");
                method.setSignature(signatureBuilder.toString());
                //System.out.println("DEBUG: " + method.getSignature());

                // Path file
                method.setMethodPath(filePath + "/" + method.getSignature());

                methods.add(method);
            });
        });

        return methods;
    }






}

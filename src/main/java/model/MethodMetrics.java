package model;

public class MethodMetrics {
    private String project;
    private String methodPath;
    private String releaseId;
    private int loc;
    private int cyclomaticComplexity;
    private int cognitiveComplexity;
    private int statements;
    private int nestingDepth;
    private int parameterCount;
    private int codeSmells;
    private boolean buggy;

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getMethodPath() { return methodPath; }
    public void setMethodPath(String methodPath) { this.methodPath = methodPath; }

    public String getReleaseId() { return releaseId; }
    public void setReleaseId(String releaseId) { this.releaseId = releaseId; }

    public int getLoc() { return loc; }
    public void setLoc(int loc) { this.loc = loc; }

    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public int getCognitiveComplexity() { return cognitiveComplexity; }
    public void setCognitiveComplexity(int cognitiveComplexity) { this.cognitiveComplexity = cognitiveComplexity; }

    public int getStatements() { return statements; }
    public void setStatements(int statements) { this.statements = statements; }

    public int getNestingDepth() { return nestingDepth; }
    public void setNestingDepth(int nestingDepth) { this.nestingDepth = nestingDepth; }

    public int getParameterCount() { return parameterCount; }
    public void setParameterCount(int parameterCount) { this.parameterCount = parameterCount; }

    public int getCodeSmells() { return codeSmells; }
    public void setCodeSmells(int codeSmells) { this.codeSmells = codeSmells; }

    public boolean isBuggy() { return buggy; }
    public void setBuggy(boolean buggy) { this.buggy = buggy; }
}

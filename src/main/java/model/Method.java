package model;

public class Method {

    private String project;
    private String methodPath;
    private String methodName;
    private String releaseId;
    private int loc;
    private int cyclomaticComplexity;
    private int cognitiveComplexity;
    private int statements;
    private int nestingDepth;
    private int parameterCount;
    private int codeSmells;
    private boolean buggy;


    private int methodHistories;

    private String signature;

    private int authors;
    private int stmtAdded;
    private int stmtDeleted;
    private int churn;
    private int cond;
    private int elseAdded;
    private int elseDeleted;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public int getMethodHistories() {
        return methodHistories;
    }

    public int getAuthors() {
        return authors;
    }

    public int getStmtAdded() {
        return stmtAdded;
    }

    public int getStmtDeleted() {
        return stmtDeleted;
    }

    public int getChurn() {
        return churn;
    }

    public int getCond() {
        return cond;
    }

    public int getElseAdded() {
        return elseAdded;
    }

    public int getElseDeleted() {
        return elseDeleted;
    }

    private String body;

    private Object startLine;
    private Object endLine;
    private String className;




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


    public void setMethodHistories(int methodHistories) {
        this.methodHistories = methodHistories;
    }

    public void setStmtAdded(int stmtAdded) {
        this.stmtAdded = stmtAdded;
    }

    public void setStmtDeleted(int stmtDeleted) {
        this.stmtDeleted = stmtDeleted;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public void setCond(int cond) {
        this.cond = cond;
    }

    public void setElseAdded(int elseAdded) {
        this.elseAdded = elseAdded;
    }

    public void setElseDeleted(int elseDeleted) {
        this.elseDeleted = elseDeleted;
    }

    public void setAuthors(int size) {
        this.authors = size;
    }

    public void setSignature(String declarationAsString) {
        this.signature = declarationAsString;
    }

    public void setBody(String s) {
        this.body = s;
    }

    public void setStartLine(Object o) {
        this.startLine = o;
    }

    public void setEndLine(Object o) {
        this.endLine = o;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSignature() {
        return signature;
    }

    public String getBody() {
        return body;
    }

    public Object getStartLine() {
        return startLine;
    }

    public Object getEndLine() {
        return endLine;
    }

    public String getClassName() {
        return className;
    }
}

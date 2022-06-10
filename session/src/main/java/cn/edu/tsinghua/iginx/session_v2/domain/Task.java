package cn.edu.tsinghua.iginx.session_v2.domain;

import cn.edu.tsinghua.iginx.session_v2.Arguments;
import cn.edu.tsinghua.iginx.thrift.DataFlowType;
import cn.edu.tsinghua.iginx.thrift.TaskType;

public class Task {

    private final TaskType taskType;

    private final DataFlowType dataFlowType;

    private final long timeout;

    private final String sql;

    private final String className;

    public Task(TaskType taskType, DataFlowType dataFlowType, long timeout, String sql, String className) {
        this.taskType = taskType;
        this.dataFlowType = dataFlowType;
        this.timeout = timeout;
        this.sql = sql;
        this.className = className;
    }

    public Task(Task.Builder builder) {
        this(builder.taskType, builder.dataFlowType, builder.timeout, builder.sql, builder.className);
    }

    public static Task.Builder builder() {
        return new Task.Builder();
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public DataFlowType getDataFlowType() {
        return dataFlowType;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getSql() {
        return sql;
    }

    public String getClassName() {
        return className;
    }

    public static class Builder {

        private TaskType taskType;

        private DataFlowType dataFlowType;

        private long timeout = Long.MAX_VALUE;

        private String sql;

        private String className;

        public Task.Builder dataFlowType(DataFlowType dataFlowType) {
            this.dataFlowType = dataFlowType;
            return this;
        }

        public Task.Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Task.Builder sql(String sql) {
            Arguments.checkTaskType(TaskType.IginX, taskType);
            this.taskType = TaskType.IginX;
            this.sql = sql;
            return this;
        }

        public Task.Builder className(String className) {
            Arguments.checkTaskType(TaskType.Python, taskType);
            this.taskType = TaskType.Python;
            this.className = className;
            return this;
        }

        public Task build() {
            Arguments.checkNotNull(taskType, "taskType");
            Arguments.checkNotNull(dataFlowType, "dataFlowType");
            return new Task(this);
        }
    }
}
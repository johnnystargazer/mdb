package com;

import com.github.myzhan.locust4j.AbstractTask;
import com.github.myzhan.locust4j.Locust;
import com.github.myzhan.locust4j.taskset.AbstractTaskSet;
import com.github.myzhan.locust4j.taskset.WeighingTaskSet;

public class TestLoast {
    public static void main(String[] args) {
        Locust locust = Locust.getInstance();
        locust.setVerbose(true);
        locust.setMaxRPS(1000);
        AbstractTaskSet taskSet = new WeighingTaskSet("test", 1);
        taskSet.addTask(new TestTask("foo", 10));
        taskSet.addTask(new TestTask("bar", 20));
        locust.run("1", taskSet);
    }

    private static class TestTask extends AbstractTask {
        public int weight;
        public String name;

        public TestTask(String name, int weight) {
            this.name = name;
            this.weight = weight;
        }

        @Override
        public int getWeight() {
            return this.weight;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public void execute() {
            System.out.println("------------------------");
            Locust.getInstance().recordSuccess("test", name, 1, 1);
        }
    }
}

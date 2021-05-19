/* Copyright 2021 ros2-java contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ros2.rcljava.examples.action;

import java.lang.System;
import java.lang.Thread;

import org.ros2.rcljava.consumers.Consumer;
import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.action.ActionServer;
import org.ros2.rcljava.action.ActionServerGoalHandle;
import org.ros2.rcljava.action.CancelCallback;
import org.ros2.rcljava.action.GoalCallback;
import org.ros2.rcljava.node.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FibonacciActionServer {
  private static final Logger logger = LoggerFactory.getLogger(FibonacciActionServer.class);

  static class FibGoalCallback implements GoalCallback<example_interfaces.action.Fibonacci.SendGoalRequest> {
    public example_interfaces.action.Fibonacci_Goal goal;
    public GoalResponse handleGoal(example_interfaces.action.Fibonacci.SendGoalRequest goal) {
      this.goal = goal.getGoal();
      return GoalResponse.ACCEPT_AND_EXECUTE;
    }
  }

  static class FibCancelCallback implements CancelCallback<example_interfaces.action.Fibonacci> {
    public ActionServerGoalHandle<example_interfaces.action.Fibonacci> goalHandle;
    public CancelResponse handleCancel(ActionServerGoalHandle<example_interfaces.action.Fibonacci> goalHandle) {
      this.goalHandle = goalHandle;
      return CancelResponse.ACCEPT;
    }
  }

  static class FibAcceptedCallback implements Consumer<ActionServerGoalHandle<example_interfaces.action.Fibonacci>> {
    public void accept(final ActionServerGoalHandle<example_interfaces.action.Fibonacci> goalHandle) {
      Thread thread = new Thread() {
        public void run() {
          logger.info("Executing goal");
          example_interfaces.action.Fibonacci_Goal goal = (example_interfaces.action.Fibonacci_Goal)goalHandle.getGoal();
          java.util.List<java.lang.Integer> sequence = new java.util.ArrayList();
          example_interfaces.action.Fibonacci_Feedback feedback =
            new example_interfaces.action.Fibonacci_Feedback();
          sequence.add(0);
          sequence.add(1);
          example_interfaces.action.Fibonacci_Result result =
            new example_interfaces.action.Fibonacci_Result();

          for (int i = 1; (i < goal.getOrder()) && RCLJava.ok(); ++i) {
            long start = System.currentTimeMillis();
            // Check if there is a cancel request
            if (goalHandle.isCanceling()) {
              result.setSequence(sequence);
              goalHandle.canceled(result);
              logger.info("Goal canceled");
              return;
            }
            // Update sequence
            sequence.add(sequence.get(i) + sequence.get(i - 1));
            // Publish feedback
            feedback.setSequence(sequence);
            goalHandle.publishFeedback(feedback);
            logger.info("Publish feedback");

            long sleepPeriod = start - System.currentTimeMillis() - 1000;
            if (sleepPeriod > 0) {
              try {
                java.lang.Thread.sleep(sleepPeriod);
              } catch (InterruptedException e) {
                // ignore
              }
            }
          }
          // Check if goal is done
          if (RCLJava.ok()) {
            result.setSequence(sequence);
            goalHandle.succeed(result);
            logger.info("Goal succeeded");
          }
        }
      };
      thread.start();
    }
  }

  public static void main(final String[] args) throws InterruptedException, Exception {
    // Initialize RCL
    RCLJava.rclJavaInit();

    // Let's create a new Node
    Node node = RCLJava.createNode("fibonacci_action_server");

    FibGoalCallback goalCallback = new FibGoalCallback();
    FibCancelCallback cancelCallback = new FibCancelCallback();
    FibAcceptedCallback acceptedCallback = new FibAcceptedCallback();
    ActionServer<example_interfaces.action.Fibonacci> actionServer =
        node.<example_interfaces.action.Fibonacci>createActionServer(
            example_interfaces.action.Fibonacci.class, "fibonacci",
            goalCallback, cancelCallback, acceptedCallback);

    RCLJava.spin(node);
    RCLJava.shutdown();
  }
}

package org.tasks.locale.receiver;

import static org.tasks.Strings.isNullOrEmpty;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import com.todoroo.astrid.dao.TaskDaoBlocking;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.data.Task.Priority;
import com.todoroo.astrid.service.TaskCreator;
import javax.inject.Inject;
import org.tasks.locale.bundle.TaskCreationBundle;
import org.tasks.time.DateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import timber.log.Timber;

class TaskerTaskCreator {

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;

  private final TaskCreator taskCreator;
  private final TaskDaoBlocking taskDao;

  @Inject
  TaskerTaskCreator(TaskCreator taskCreator, TaskDaoBlocking taskDao) {
    this.taskCreator = taskCreator;
    this.taskDao = taskDao;
  }

  public void handle(TaskCreationBundle bundle) {
    Task task = taskCreator.basicQuickAddTask(bundle.getTitle());

    String dueDateString = bundle.getDueDate();
    if (!isNullOrEmpty(dueDateString)) {
      try {
        LocalDate dueDate = LocalDate.parse(dueDateString, dateFormatter);
        DateTime dt =
            new DateTime(dueDate.getYear(), dueDate.getMonthValue(), dueDate.getDayOfMonth());
        task.setDueDate(Task.createDueDate(Task.URGENCY_SPECIFIC_DAY, dt.getMillis()));
      } catch (Exception e) {
        Timber.e(e);
      }
    }

    String dueTimeString = bundle.getDueTime();
    if (!isNullOrEmpty(dueTimeString)) {
      try {
        LocalTime dueTime = LocalTime.parse(dueTimeString, timeFormatter);
        task.setDueDate(
            Task.createDueDate(
                Task.URGENCY_SPECIFIC_DAY_TIME,
                new DateTime(task.hasDueDate() ? task.getDueDate() : currentTimeMillis())
                    .withHourOfDay(dueTime.getHour())
                    .withMinuteOfHour(dueTime.getMinute())
                    .getMillis()));
      } catch (Exception e) {
        Timber.e(e);
      }
    }

    String priorityString = bundle.getPriority();
    if (!isNullOrEmpty(priorityString)) {
      try {
        int priority = Integer.parseInt(priorityString);
        task.setPriority(Math.max(Priority.HIGH, Math.min(Priority.NONE, priority)));
      } catch (NumberFormatException e) {
        Timber.e(e);
      }
    }

    task.setNotes(bundle.getDescription());

    taskDao.save(task);

    taskCreator.createTags(task);
  }
}

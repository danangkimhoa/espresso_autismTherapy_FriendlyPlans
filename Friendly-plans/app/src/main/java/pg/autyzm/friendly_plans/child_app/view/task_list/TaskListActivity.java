package pg.autyzm.friendly_plans.child_app.view.task_list;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import database.entities.ChildPlan;
import database.entities.TaskTemplate;
import database.repository.ChildPlanRepository;
import pg.autyzm.friendly_plans.ActivityProperties;
import pg.autyzm.friendly_plans.App;
import pg.autyzm.friendly_plans.R;
import pg.autyzm.friendly_plans.child_app.utility.ChildActivityExecutor;
import pg.autyzm.friendly_plans.child_app.utility.Consts;
import pg.autyzm.friendly_plans.child_app.view.common.ChildActivityState;
import pg.autyzm.friendly_plans.child_app.view.step_list.StepListActivity;

public class TaskListActivity extends AppCompatActivity {
    @Inject
    ChildPlanRepository childPlanRepository;
    private TaskRecyclerViewAdapter taskRecyclerViewAdapter;
    static final int RESULT_CODE = 1;

    TaskRecyclerViewAdapter.TaskItemClickListener taskItemClickListener =
            new TaskRecyclerViewAdapter.TaskItemClickListener() {

                @Override
                public void stepsIconListener(int position) {
                    if (position == taskRecyclerViewAdapter.getCurrentTaskPosition()
                            && taskRecyclerViewAdapter.getCurrentTaskState() != ChildActivityState.IN_PROGRESS)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putLong(ActivityProperties.TASK_ID,
                                taskRecyclerViewAdapter.getTaskItem(position).getId());

                        Intent intent = new Intent(TaskListActivity.this, StepListActivity.class);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, RESULT_CODE);
                    }
                }

                @Override
                public void timerIconClickListener(int clickPosition, final TextView durationLabel) {
                    if (clickPosition != taskRecyclerViewAdapter.getCurrentTaskPosition()
                            || taskRecyclerViewAdapter.getCurrentTaskState() == ChildActivityState.IN_PROGRESS)
                        return;

                    if (taskRecyclerViewAdapter.getCurrentTaskState() == ChildActivityState.FINISHED){
                        if (clickPosition < taskRecyclerViewAdapter.getItemCount() - 1) {
                            taskRecyclerViewAdapter.setCurrentTask(clickPosition + 1);
                            return;
                        }
                        finish();
                    }
                    startChildActivityExecution(durationLabel);
                }

                @Override
                public void blankChildActivityListener(int clickPosition) {
                    if (clickPosition != taskRecyclerViewAdapter.getCurrentTaskPosition())
                        return;
                    if (clickPosition < taskRecyclerViewAdapter.getItemCount() - 1) {
                        taskRecyclerViewAdapter.moveToNextTask();
                        return;
                    }
                    finish();
                }

                private void startChildActivityExecution(final TextView durationLabel) {
                    taskRecyclerViewAdapter.setCurrentTaskState(ChildActivityState.IN_PROGRESS);
                    final Handler timerHandler = new Handler();
                    String durationStr = durationLabel.getText().toString();
                    Integer duration = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));

                    Runnable updater = new ChildActivityExecutor(duration, durationLabel, timerHandler, taskRecyclerViewAdapter);
                    timerHandler.post(updater);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((App) getApplication()).getAppComponent().inject(this);
        setContentView(R.layout.child_app_task_list);
        setRecyclerView();
    }

    void setRecyclerView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_child_app_task_list);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        ChildPlan activePlan = childPlanRepository.getActivePlan();
        List<TaskTemplate> tasks = activePlan.getPlanTemplate().getTasksWithThisPlan();
        String filesDirectory = getApplicationContext().getFilesDir().toString();
        taskRecyclerViewAdapter = new TaskRecyclerViewAdapter(tasks, taskItemClickListener, filesDirectory);

        recyclerView.setAdapter(taskRecyclerViewAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && data.getStringExtra(Consts.RETURN_MESSAGE_KEY) != null)
            taskRecyclerViewAdapter.moveToNextTask();
    }
}

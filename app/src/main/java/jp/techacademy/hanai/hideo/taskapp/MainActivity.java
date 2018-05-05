package jp.techacademy.hanai.hideo.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.hanai.hideo.taskapp.TASK";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;

    private InputMethodManager inputMethodManager;
    private android.support.design.widget.CoordinatorLayout mainLayout;
    private EditText mSelectc;
    public String cText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //　カテゴリーを入力（Enter)した時の処理
        mSelectc = (EditText) findViewById(R.id.selectc_edit_text);
        mainLayout = (android.support.design.widget.CoordinatorLayout)findViewById(R.id.mainLayout);
        inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);




        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        // Realmの設定
//        Realm.init(this);  //Realm初期化
//        RealmConfiguration config = new RealmConfiguration.Builder().build();
//        Realm.setDefaultConfiguration(config);
//      Realm.deleteRealm(congfig);
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        //EditTextにリスナーをセット
        mSelectc.setOnKeyListener(new View.OnKeyListener() {

            //コールバックとしてonKey()メソッドを定義
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //キーが押された時
                if((event.getAction() == KeyEvent.ACTION_DOWN) ){
                    //　押されたキーがEnterだった時
                    if(keyCode == KeyEvent.KEYCODE_ENTER){
                       //キーボードを閉じる
                        inputMethodManager.hideSoftInputFromWindow(mSelectc.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
                        reloadListView();
                        return true;
                    }
                }

                return false;
            }
        });


        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }

    private void reloadListView() {
        //　カテゴリーの文字列を取得
        SpannableStringBuilder sb = (SpannableStringBuilder)mSelectc.getText();
        cText=sb.toString();
        //c = toString(mSelectc.getText());

        if (cText.length()==0){
            // Realmデータベースから、「新しい日時順に並べた結果」を取得
            RealmResults<Task> taskRealmResults = mRealm.where(Task.class)
                    .findAllSorted("date", Sort.DESCENDING);

            // 上記の結果を、TaskList としてセットする
            mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
            // TaskのListView用のアダプタに渡す
            mListView.setAdapter(mTaskAdapter);
            // 表示を更新するために、アダプターにデータが変更されたことを知らせる
            mTaskAdapter.notifyDataSetChanged();
        } else {

            // Realmデータベースから、「カテゴリーで抽出後、新しい日時順に並べた結果」を取得
            RealmResults<Task> taskRealmResults = mRealm.where(Task.class)
                    .equalTo("category", cText).findAllSorted("date", Sort.DESCENDING);

            // 上記の結果を、TaskList としてセットする
            mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
            // TaskのListView用のアダプタに渡す
            mListView.setAdapter(mTaskAdapter);
            // 表示を更新するために、アダプターにデータが変更されたことを知らせる
            mTaskAdapter.notifyDataSetChanged();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }
}
package cmg.com.basesample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Bind(R.id.rx_editText)
    EditText editText;

    @Bind(R.id.content_main)
    View root;

    Subscription editTextSubscription;
    Single<String> singleOperationObservable;
    Subscription singleOperationSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

        // Observable simple
        Observable.just("A", "B")
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d(TAG, s);
                    }
                });


        List<Integer> items = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

        //Working with collections
        Observable
                .from(items)
                .map(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer integer) {
                        return integer * integer;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        Log.d(TAG, "onNext integer: " + integer);
                    }
                })
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        Log.d(TAG, "onCompleted list iteration");
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.d(TAG, "Achtung!!!");
                    }
                })
                .subscribe();


        // Subscribe on long time operation
        singleOperationObservable = Single.create(new Single.OnSubscribe<String>() {
            @Override
            public void call(SingleSubscriber singleSubscriber) {
                String value = longRunningOperation();
                singleSubscriber.onSuccess(value);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(new Action1<String>() {
                    @Override
                    public void call(String string) {
                        snack(string);
                    }
                }).doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        snack("Error: " + throwable.getLocalizedMessage());
                    }
                });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snack("Long operation started");
                singleOperationSubscription = singleOperationObservable.subscribe();
            }
        });


        // Work with views, debounce
        editTextSubscription = RxTextView.textChanges(editText).debounce(1000, TimeUnit.MILLISECONDS).subscribe(new Action1<CharSequence>() {
            @Override
            public void call(CharSequence charSequence) {
                String text = charSequence.toString();
                if (!text.isEmpty()) snack(text);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (editTextSubscription != null && !editTextSubscription.isUnsubscribed()) {
            Log.d(TAG, "Unsubscribing editTextSubscription");
            editTextSubscription.unsubscribe();
        }
        if (singleOperationSubscription != null && !singleOperationSubscription.isUnsubscribed()) {
            Log.d(TAG, "Unsubscribing singleOperationSubscription"); // Will not be called because Single
            singleOperationSubscription.unsubscribe();
        }
    }

    public String longRunningOperation() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // error
        }
        return "Complete!";
    }

    private void snack(String that) {
        Snackbar.make(root, that, Snackbar.LENGTH_SHORT).show();
    }
}

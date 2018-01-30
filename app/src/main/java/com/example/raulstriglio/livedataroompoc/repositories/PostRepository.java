package com.example.raulstriglio.livedataroompoc.repositories;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import com.example.modelviewviewmodel.repository.UseCaseRepository;
import com.example.raulstriglio.livedataroompoc.db.AppDatabase;
import com.example.raulstriglio.livedataroompoc.db.entities.Post;
import com.example.raulstriglio.livedataroompoc.services.JobManagerFactory;
import com.example.raulstriglio.livedataroompoc.services.PostApiService;
import com.example.raulstriglio.livedataroompoc.services.PostJob;

import org.w3c.dom.ls.LSException;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by raul.striglio on 18/01/18.
 */

public class PostRepository extends UseCaseRepository<Post> {

    private AppDatabase mDataBase;
    private PostApiService mClient;
    private CompositeDisposable disposable;
    private String mUserId;


    @Inject
    public PostRepository(Application context, PostApiService client){
        super(context);
        mContext = context;
        mClient = client;
        disposable = new CompositeDisposable();
    }

    @Override
    public void initLocalData() {
        mDataBase = AppDatabase.getInMemoryDatabase(mContext);
        setDataList(mDataBase.postsModel().loadAllPosts());
    }

    @Override
    public void addData(Post post) {
        mDataBase.postsModel().insertPost(post);
        //add post job to priority queue
        PostJob postJob = new PostJob(post);
        JobManagerFactory.getJobManager().addJobInBackground(postJob);
    }

    @Override
    public void addDataList(List<Post> dataList) {
        mDataBase.postsModel().insertAll(dataList);
        setDataList(mDataBase.postsModel().loadAllPosts());
    }

    public void setPostsDataListByUserId(String userId){
        MutableLiveData<List<Post>> listMutableLiveData = new MutableLiveData<>();
        listMutableLiveData.setValue(getPostFromDbByUserId(userId));
        setDataList(listMutableLiveData);
    }

    public List<Post> getPostFromDbByUserId(String userId){
        return mDataBase.postsModel().loadPostsByUser(userId);
    }

    @Override
    public void requestDataToServer() {
        mClient.getPosById(mUserId).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Post>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(List<Post> posts) {
                        mDataBase.postsModel().insertAll(posts);
                        setPostsDataListByUserId(mUserId);
                        getDataList();
                        disposable.dispose();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("error", e.getMessage());
                        //manage error
                        disposable.dispose();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    public void requestPostsToServerByUser(final String userId) {
        mUserId = userId;
        requestDataToServer();
    }

    public void updatePost(Post post) {
        mDataBase.postsModel().insertPost(post);
    }

    public void deletePost(Post post) {
        mDataBase.postsModel().deletePost(post);
    }

    public Post loadPost(Post post){
        return mDataBase.postsModel().loadPost(post.getId());
    }
}

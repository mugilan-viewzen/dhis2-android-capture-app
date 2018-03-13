package com.dhis2.usescases.login;


import android.databinding.ObservableField;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.dhis2.databinding.ActivityLoginBinding;
import com.dhis2.usescases.general.AbstractActivityContracts;

import org.hisp.dhis.android.core.user.User;

import io.reactivex.functions.Consumer;
import retrofit2.Response;

public class LoginContracts {

    public interface View extends AbstractActivityContracts.View {
        ActivityLoginBinding getBinding();

        @UiThread
        void showProgress();

        @UiThread
        void hideProgress();

        @UiThread
        void renderInvalidServerUrlError();

        @UiThread
        void renderInvalidCredentialsError();

        @UiThread
        void renderUnexpectedError();

        @UiThread
        void renderServerError();

        @UiThread
        void handleSync();

        @UiThread
        void onUnlockClick(android.view.View android);

        @UiThread
        void setAutocompleteAdapters();

        @UiThread
        void saveUsersData();

        @NonNull
        Consumer<SyncResult> update(LoginActivity.SyncState syncState);
    }

    public interface Presenter {
        void init(View view);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void onButtonClick();

        void onQRClick(android.view.View v);

        ObservableField<Boolean> isServerUrlSet();

        ObservableField<Boolean> isUserNameSet();

        ObservableField<Boolean> isUserPassSet();

        void unlockSession(String pin);

        void onDestroy();

        void syncNext(LoginActivity.SyncState syncState);
    }

    public interface Interactor {
        void validateCredentials(@NonNull String serverUrl,
                                 @NonNull String username, @NonNull String password);

        void handleResponse(@NonNull Response<User> userResponse);

        void handleError(@NonNull Throwable throwable);


        void sync();

        void syncEvents();

        void syncTrackedEntities();

        void onDestroy();
    }

}
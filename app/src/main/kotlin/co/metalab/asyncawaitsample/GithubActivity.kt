package co.metalab.asyncawaitsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import co.metalab.asyncawait.RetrofitHttpError
import co.metalab.asyncawait.async
import co.metalab.asyncawait.awaitSuccessful
import co.metalab.service.GitHubService
import co.metalab.service.GithubErrorResponse
import co.metalab.service.Repo
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_github.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GitHubActivity : AppCompatActivity() {
    private val TAG = GitHubActivity::class.java.simpleName

    var retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    var github = retrofit.create(GitHubService::class.java)

    var reposList = emptyList<Repo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_github)
        btnGetRepos.setOnClickListener { refreshRepos() }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }

    private fun refreshRepos() = async {
        showLoadingUi()

        val userName = txtUserName.text.toString()
        reposList = awaitSuccessful(github.listRepos(userName))
        showRepos(reposList)

        progressBar.isIndeterminate = false
        reposList.forEachIndexed { i, repo ->
            txtStatus.text = "Loading info for ${repo.name}..."
            progressBar.progress = i * 100 / reposList.size
            val repoDetails = awaitSuccessful(github.repoDetails(userName, repo.name))
            repo.stars = repoDetails.stargazers_count
            showRepos(reposList)
        }

        txtStatus.text = "Done."
    }.onError {
        val errorMessage = getErrorMessage(it.cause!!)

        txtStatus.text = errorMessage
        Log.e(TAG, errorMessage, it)
    }.finally {
        progressBar.visibility = View.INVISIBLE
        btnGetRepos.isEnabled = true
    }

    private fun showLoadingUi() {
        txtRepos.text = ""
        progressBar.isIndeterminate = true
        progressBar.visibility = View.VISIBLE
        btnGetRepos.isEnabled = false
        txtStatus.text = "Loading repos list..."
    }

    private fun getErrorMessage(it: Throwable): String {
        return if (it is RetrofitHttpError) {
            val httpErrorCode = it.errorResponse.code()
            val errorResponse = Gson().fromJson(it.errorResponse.errorBody().string(), GithubErrorResponse::class.java)
            "[$httpErrorCode] ${errorResponse.message}"
        } else {
            "Couldn't load repos (${it.message})"
        }
    }

    private fun showRepos(reposResponse: List<Repo>) {
        val reposList = reposResponse.joinToString(separator = "\n") {
            val starsCount = if (it.stars == null) "" else {
                " * ${it.stars}"
            }
            it.name + starsCount
        }
        txtRepos.text = reposList
    }
}
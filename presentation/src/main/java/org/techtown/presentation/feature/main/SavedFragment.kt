package org.techtown.presentation.feature.main

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.androidcleanarchitecturecoroutine.R
import org.techtown.androidcleanarchitecturecoroutine.databinding.FragmentSavedBinding
import org.techtown.presentation.base.BaseFragment
import org.techtown.presentation.database.database.AppDatabase
import org.techtown.presentation.ext.navigateWithAnim
import org.techtown.presentation.feature.main.adapter.TopNewsAdapter
import org.techtown.presentation.model.Articles

class SavedFragment : BaseFragment<FragmentSavedBinding>(R.layout.fragment_saved) {

    private lateinit var savedNewsAdapter: TopNewsAdapter

    private lateinit var navController: NavController
    private lateinit var navHost: NavHostFragment

    private var shouldRequestViewMore: Boolean = true

    private var tempSavedArticleList: ArrayList<Articles> = arrayListOf()
    var recyclerViewScrollState: Parcelable? = null

    private lateinit var database: AppDatabase

    override fun FragmentSavedBinding.onCreateView() {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {// 데이터값 날라가는 경우 주의.
            recyclerViewScrollState = savedInstanceState.getParcelable("recyclerview_state")
            tempSavedArticleList = savedInstanceState.getParcelableArrayList("recyclerview_list")!!
        }
        initSet()
    }

    private fun initSet() {

        //db setting
        database = AppDatabase.getInstance(requireActivity().applicationContext)

        //로컬 디비에 있는 리스트 가지고옴.
        getSavedArticleList()

        navHost =
            requireActivity().supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        navController = navHost.findNavController()

        //스크롤 유지.
        binding.rvSavedNews.layoutManager?.onRestoreInstanceState(recyclerViewScrollState)
    }

    private fun getSavedArticleList() {

        CoroutineScope(Dispatchers.IO).launch {
            val savedArticles = database.articleDao().getAllArticles()

            CoroutineScope(Dispatchers.Main).launch {
                if (savedArticles.isNotEmpty()) {

                    savedNewsAdapter = TopNewsAdapter()
                    binding.rvSavedNews.apply {
                        adapter = savedNewsAdapter
                    }

                    if (tempSavedArticleList.size > 0) {
                        if (tempSavedArticleList[tempSavedArticleList.lastIndex].isLoading) {
                            tempSavedArticleList.removeAt(tempSavedArticleList.lastIndex)
                            savedNewsAdapter.submitList(tempSavedArticleList.map { it.copy() })
                        }
                    }

                    tempSavedArticleList.clear()
                    tempSavedArticleList.addAll(savedArticles)
                    savedNewsAdapter.submitList(tempSavedArticleList.map { it.copy() }
                        .toMutableList())

                    setListenerEvent()
                } else {
                    shouldRequestViewMore = false
                }
            }
        }
    }

    private fun setListenerEvent() {
        savedNewsAdapter.setItemClickListener(object : TopNewsAdapter.ItemClickListener {
            override fun onItemClick(articles: Articles) {
                navController.navigateWithAnim(R.id.topNews_detail, Bundle().apply {
                    putParcelable("top_news_detail", articles)
                })
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("recyclerview_state", recyclerViewScrollState)
        outState.putParcelableArrayList("recyclerview_list", tempSavedArticleList)
    }

}
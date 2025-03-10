package org.techtown.presentation.feature.main

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.techtown.androidcleanarchitecturecoroutine.R
import org.techtown.androidcleanarchitecturecoroutine.databinding.FragmentCategoryDetailNewsBinding
import org.techtown.presentation.base.BaseFragment
import org.techtown.presentation.database.database.AppDatabase
import org.techtown.presentation.ext.navigateWithAnim
import org.techtown.presentation.feature.main.adapter.TopNewsAdapter
import org.techtown.presentation.model.Articles
import org.techtown.presentation.repository.NewsRepository
import org.techtown.presentation.repository.NewsRepositoryImpl
import org.techtown.presentation.retrofit.NewsService

class CategoryDetailNewsFragment :
    BaseFragment<FragmentCategoryDetailNewsBinding>(R.layout.fragment_category_detail_news) {

    private lateinit var categoryNewsAdapter: TopNewsAdapter

    private lateinit var navController: NavController
    private lateinit var navHost: NavHostFragment

    private var offset = 1
    private var limit = 5

    private var shouldRequestViewMore: Boolean = true

    private var tempCategoryList: ArrayList<Articles> = arrayListOf()
    var recyclerViewScrollState: Parcelable? = null

    private lateinit var category: String

    //db setting
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(requireActivity().applicationContext)
    }

    private val newsRepository: NewsRepository by lazy {
        val newsService = NewsService.apiService
        NewsRepositoryImpl(newsService, database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun FragmentCategoryDetailNewsBinding.onCreateView() {
        initSet()
    }

    private fun initSet() {

        arguments?.let {
            category = it.getString("category_detail") ?: ""
        }

        if (tempCategoryList.isEmpty() && !this::categoryNewsAdapter.isInitialized) {
            //카테고리 상세 리스트를 가져옵니다.
            getCategoryArticles()
        } else {
            binding.rvCategoryDetail.apply {
                adapter = categoryNewsAdapter
            }
            categoryNewsAdapter.submitList(tempCategoryList)
        }


        navHost =
            requireActivity().supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        navController = navHost.findNavController()

        binding.rvCategoryDetail.layoutManager?.onRestoreInstanceState(recyclerViewScrollState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null) {
            recyclerViewScrollState = savedInstanceState.getParcelable("recyclerview_state")
            tempCategoryList = savedInstanceState.getParcelableArrayList("recyclerview_list")!!
        }
    }

    private fun setListenerEvent() {

        //뉴스 클릭 이벤트.
        categoryNewsAdapter.setItemClickListener(object : TopNewsAdapter.ItemClickListener {
            override fun onItemClick(articles: Articles) {
                navController.navigateWithAnim(R.id.topNews_detail, Bundle().apply {
                    putParcelable("top_news_detail", articles)
                })
            }
        })

        binding.rvCategoryDetail.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisiblePosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val itemTotalCount = recyclerView.adapter?.itemCount?.minus(1)

                recyclerViewScrollState =
                    binding.rvCategoryDetail.layoutManager?.onSaveInstanceState()

                if (!recyclerView.canScrollVertically(1)
                    && lastVisiblePosition == itemTotalCount
                    && shouldRequestViewMore
                ) {
                    getCategoryArticles()
                }

                if (!shouldRequestViewMore) {
                    categoryNewsAdapter.deleteLoading()
                }

            }
        })
    }

    private fun getCategoryArticles() {


        if (tempCategoryList.isNotEmpty()) {
            if (!tempCategoryList[tempCategoryList.lastIndex].isLoading) {
                tempCategoryList.add(Articles(isLoading = true, title = "", url = ""))
                categoryNewsAdapter.submitList(tempCategoryList.map { it.copy() }.toMutableList())
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            newsRepository.getTopHeadlinesArticles(
                "us",
                category = category,
                limit,
                offset
            ).collect { data ->

                CoroutineScope(Dispatchers.Main).launch {
                    if (!this@CategoryDetailNewsFragment::categoryNewsAdapter.isInitialized) {
                        categoryNewsAdapter = TopNewsAdapter()
                        binding.rvCategoryDetail.apply {
                            adapter = categoryNewsAdapter
                        }
                    }

                    if (tempCategoryList.size > 0) {
                        if (tempCategoryList[tempCategoryList.lastIndex].isLoading) {
                            tempCategoryList.removeAt(tempCategoryList.lastIndex)
                            categoryNewsAdapter.submitList(tempCategoryList.map { it.copy() })
                        }
                    }

                    tempCategoryList.addAll(data.articles)
                    categoryNewsAdapter.submitList(tempCategoryList.map { it.copy() }
                        .toMutableList())

                    setListenerEvent()

                    offset += 1
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("recyclerview_state", recyclerViewScrollState)
        outState.putParcelableArrayList("recyclerview_list", tempCategoryList)
    }
}
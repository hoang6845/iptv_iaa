package com.iptvplayer.m3u.stream.ui.favourite_xtream

import androidx.lifecycle.viewModelScope
import com.iptvplayer.m3u.stream.model.dao.FavouriteDao
import com.iptvplayer.m3u.stream.model.entity.Favourite
import com.iptvplayer.m3u.stream.model.entity.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouriteXtreamViewModel @Inject constructor(
    private val favouriteDao: FavouriteDao
) : BaseViewModel() {

    val favouriteMovies: Flow<List<SearchResult>> =
        favouriteDao.getFavouriteMovies()

    val favouriteSeries: Flow<List<SearchResult>> =
        favouriteDao.getFavouriteSeries()

    val favouriteLives: Flow<List<SearchResult>> =
        favouriteDao.getFavouriteLives()

    fun addFavourite(server: Int, uniqueId: String) {
        viewModelScope.launch {
            favouriteDao.addFavourite(Favourite(server, uniqueId))
        }
    }

    fun removeFavourite(server: Int, uniqueId: String) {
        viewModelScope.launch {
            favouriteDao.removeFavourite(server, uniqueId)
        }
    }
}
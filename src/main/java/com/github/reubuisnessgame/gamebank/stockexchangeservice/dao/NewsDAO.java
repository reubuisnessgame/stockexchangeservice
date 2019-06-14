package com.github.reubuisnessgame.gamebank.stockexchangeservice.dao;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.NewsModel;
import com.github.reubuisnessgame.gamebank.stockexchangeservice.repository.NewsRepository;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@Component
public class NewsDAO {

    private final NewsRepository newsRepository;

    public NewsDAO(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    public NewsModel createNews(String heading, String article) {
        return newsRepository.save(new NewsModel(heading, article,
                StockExchangeDAO.simpleDateFormat.format(new Date()), System.currentTimeMillis()));
    }

    public ArrayList<NewsModel> getAllNews() throws NotFoundException {
        Iterable<NewsModel> newsModels = newsRepository.findAll();
        if (!newsModels.iterator().hasNext()) {
            throw new NotFoundException("News not found");
        }
        ArrayList<NewsModel> newsModelArrayList = new ArrayList<>();
        newsModels.forEach((newsModelArrayList::add));
        newsModelArrayList.sort((n1, n2) -> (int) (n1.getTimeMillis() - n2.getTimeMillis()));
        return newsModelArrayList;
    }

    public void clearAll() {
        newsRepository.deleteAll();
    }

}

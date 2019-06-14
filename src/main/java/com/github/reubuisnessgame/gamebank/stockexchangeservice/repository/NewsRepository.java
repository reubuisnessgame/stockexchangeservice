package com.github.reubuisnessgame.gamebank.stockexchangeservice.repository;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.NewsModel;
import org.springframework.data.repository.CrudRepository;

public interface NewsRepository extends CrudRepository<NewsModel, Long> {
}

package com.github.reubuisnessgame.gamebank.stockexchangeservice.repository;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.ChangingPriceModel;
import org.springframework.data.repository.CrudRepository;

public interface ChangingPriceRepository extends CrudRepository<ChangingPriceModel, Long> {
    Iterable<ChangingPriceModel> findAllByCompanyId(Long companyId);
    void deleteAllByCompanyId(Long companyId);
}

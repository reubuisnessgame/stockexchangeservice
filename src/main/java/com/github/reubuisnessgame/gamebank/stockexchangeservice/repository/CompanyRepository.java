package com.github.reubuisnessgame.gamebank.stockexchangeservice.repository;

import com.github.reubuisnessgame.gamebank.stockexchangeservice.model.CompanyModel;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CompanyRepository extends CrudRepository<CompanyModel, Long> {
    Optional<CompanyModel> findByCompanyName(String companyName);
}

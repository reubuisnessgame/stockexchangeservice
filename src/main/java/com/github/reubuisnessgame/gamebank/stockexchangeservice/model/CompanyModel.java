package com.github.reubuisnessgame.gamebank.stockexchangeservice.model;

import javax.persistence.*;

@Entity
@Table(name = "company")
public class CompanyModel {

    @Id
    @GeneratedValue
    @Column(name = "company_id", unique = true)
    private Long id;

    @Column(name = "share_price")
    private Double sharePrice;

    //Json Ignore
    @Column(name = "stable_price")
    private double stablePrice;

    @Column(name = "company_name", unique = true, nullable = false)
    private String companyName;

    @Column(name = "full_count", nullable = false)
    private long fullCount;

    @Column(name = "free_count")
    private long freeCount;

    public CompanyModel() {
    }

    public CompanyModel(Double sharePrice, String companyName, long fullCount) {
        this.sharePrice = sharePrice;
        stablePrice = sharePrice;
        this.companyName = companyName;
        this.fullCount = fullCount;
        freeCount = fullCount;
    }

    public Long getId() {
        return id;
    }

    public Double getSharePrice() {
        return sharePrice;
    }

    public void setSharePrice(Double sharePrice) {
        this.sharePrice = sharePrice;
    }

    public double getStablePrice() {
        return stablePrice;
    }

    public void setStablePrice(double stablePrice) {
        this.stablePrice = stablePrice;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public long getFullCount() {
        return fullCount;
    }

    public void setFullCount(long fullCount) {
        this.fullCount = fullCount;
    }

    public long getFreeCount() {
        return freeCount;
    }

    public void setFreeCount(long freeCount) {
        this.freeCount = freeCount;
    }
}

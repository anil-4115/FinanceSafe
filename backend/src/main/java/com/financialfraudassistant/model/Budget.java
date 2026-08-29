package com.financialfraudassistant.model;
import jakarta.persistence.*; import java.math.BigDecimal;
@Entity @Table(name="budgets", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","category"}))
public class Budget { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id; @ManyToOne(optional=false) @JoinColumn(name="user_id") private User user; @Column(nullable=false) private String category; @Column(name="monthly_limit",nullable=false) private BigDecimal monthlyLimit; protected Budget(){} public Budget(User u,String c,BigDecimal l){user=u;category=c;monthlyLimit=l;} public Integer getId(){return id;} public String getCategory(){return category;} public BigDecimal getMonthlyLimit(){return monthlyLimit;} public void update(BigDecimal l){monthlyLimit=l;} }

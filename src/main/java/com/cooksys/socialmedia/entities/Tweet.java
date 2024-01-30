package com.cooksys.socialmedia.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.sql.Timestamp;
import java.util.*;

@Entity
@NoArgsConstructor
@Data
public class Tweet {

  @Id
  @GeneratedValue
  private Long id;
  
  @ManyToOne
  @NonNull
  @JoinColumn(name = "User_id")
  private User Author;
  
  private Timestamp posted;
  
  private String content;
  
  private Boolean deleted;
  
  @OneToOne
  @JoinColumn(name = "Tweet_id")
  private Tweet inReplyTo;
  
  @ManyToOne
  @JoinColumn(name = "Tweet_id")
  private Tweet repostOf;
}
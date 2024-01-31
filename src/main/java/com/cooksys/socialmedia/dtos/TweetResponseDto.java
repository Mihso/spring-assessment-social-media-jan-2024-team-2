package com.cooksys.socialmedia.dtos;

import java.sql.Timestamp;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class TweetResponseDto {

    private Long id;

    private UserResponseDto Author;

    private Timestamp posted;
    
    private String content;

    private TweetResponseDto inReplyTo;

    private TweetResponseDto repostOf;

}
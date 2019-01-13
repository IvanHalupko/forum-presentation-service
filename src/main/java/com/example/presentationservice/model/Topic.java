package com.example.presentationservice.model;

import lombok.Data;

import java.util.List;

@Data
public class Topic {

    private String id;
    private String topicName;
    private Long topicDate;
    private String userId;
    private String category;
    private String userName;
    private List<Answer> answers;
}

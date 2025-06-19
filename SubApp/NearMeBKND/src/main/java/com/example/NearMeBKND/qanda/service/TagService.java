package com.example.NearMeBKND.qanda.service;

import com.example.NearMeBKND.qanda.model.Tag;
import com.example.NearMeBKND.qanda.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }
}
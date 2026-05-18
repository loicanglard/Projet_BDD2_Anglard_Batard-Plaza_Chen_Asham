package com.project.artconnect.dao;

import com.project.artconnect.model.Exhibition;
import java.util.List;

public interface ExhibitionDao {
    List<Exhibition> findAll();

    List<Exhibition> findByGalleryName(String galleryName);

    void save(Exhibition exhibition);

    void update(Exhibition exhibition);

    void delete(String title);
}

package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ExhibitionDao;
import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;

import java.util.List;
import java.util.Optional;

public class JdbcGalleryService implements GalleryService {

    private final GalleryDao galleryDao;
    private final ExhibitionDao exhibitionDao;

    public JdbcGalleryService(GalleryDao galleryDao, ExhibitionDao exhibitionDao) {
        this.galleryDao = galleryDao;
        this.exhibitionDao = exhibitionDao;
    }

    @Override
    public List<Gallery> getAllGalleries() {
        List<Gallery> galleries = galleryDao.findAll();
        for (Gallery gallery : galleries) {
            List<Exhibition> exhibitions = exhibitionDao.findByGalleryName(gallery.getName());
            for (Exhibition exhibition : exhibitions) exhibition.setGallery(gallery);
            gallery.setExhibitions(exhibitions);
        }
        return galleries;
    }

    @Override
    public Optional<Gallery> getGalleryByName(String name) {
        return getAllGalleries().stream().filter(g -> g.getName().equals(name)).findFirst();
    }

    @Override
    public List<Exhibition> getExhibitionsByGallery(Gallery gallery) {
        if (gallery == null) return List.of();
        return exhibitionDao.findByGalleryName(gallery.getName());
    }

    @Override
    public void createGallery(Gallery gallery) {
        galleryDao.save(gallery);
    }

    @Override
    public void updateGallery(Gallery gallery) {
        galleryDao.update(gallery);
    }

    @Override
    public void deleteGallery(String name) {
        galleryDao.delete(name);
    }
}

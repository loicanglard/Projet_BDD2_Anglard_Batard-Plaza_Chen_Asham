package com.project.artconnect.util;

import com.project.artconnect.dao.*;
import com.project.artconnect.persistence.*;
import com.project.artconnect.service.*;
import com.project.artconnect.service.impl.*;

public class ServiceProvider {

    private static final ArtistDao      artistDao      = new JdbcArtistDao();
    private static final ArtworkDao     artworkDao     = new JdbcArtworkDao();
    private static final GalleryDao     galleryDao     = new JdbcGalleryDao();
    private static final WorkshopDao    workshopDao    = new JdbcWorkshopDao();
    private static final ExhibitionDao  exhibitionDao  = new JdbcExhibitionDao();
    private static final CommunityMemberDao memberDao  = new JdbcCommunityMemberDao();

    private static final ArtistService    artistService    = new JdbcArtistService(artistDao);
    private static final ArtworkService   artworkService   = new JdbcArtworkService(artworkDao);
    private static final GalleryService   galleryService   = new JdbcGalleryService(galleryDao, exhibitionDao);
    private static final WorkshopService  workshopService  = new JdbcWorkshopService(workshopDao);
    private static final CommunityService communityService = new JdbcCommunityService(memberDao);

    public static ArtistService getArtistService() {
        return artistService;
    }

    public static ArtworkService getArtworkService() {
        return artworkService;
    }

    public static GalleryService getGalleryService() {
        return galleryService;
    }

    public static WorkshopService getWorkshopService() {
        return workshopService;
    }

    public static CommunityService getCommunityService() {
        return communityService;
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;

@Entity
@Table(name="galleryConfigParameter")
public class GalleryConfigParameter {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="gallery_id", nullable=false)
    private GalleryConfig gallery;
    @Column
    private String name;
    @Column
    private String category;
    @Column
    private String value;

    public Long getId() {
        return this.id;
    }

    public GalleryConfig getGallery() {
        return this.gallery;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return this.category;
    }

    public String getValue() {
        return this.value;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setGallery(GalleryConfig gallery) {
        this.gallery = gallery;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setValue(String value) {
        this.value = value;
    }
}


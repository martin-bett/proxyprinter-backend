package my.poo.mtgcrawl.mtgprint.entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "galleryConfigParameter")
public class GalleryConfigParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    

}
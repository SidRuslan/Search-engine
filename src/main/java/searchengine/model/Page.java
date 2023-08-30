package searchengine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = "pages", indexes = {@Index(name = "idx_path", columnList = "path, site_id")})
public class Page implements Comparable<Page>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id",referencedColumnName = "id", nullable = false)
    private Site site;


    @Column(columnDefinition = "VARCHAR(500)", nullable = false)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToMany()
    @JoinTable(name = "index_model",
            joinColumns = {@JoinColumn(name = "page_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")})
    private List<Lemma> lemmaList;

    @OneToMany(mappedBy = "page")
    private List<IndexModel> indexModels;

    @Override
    public int compareTo(Page o) {

       return path.compareTo(o.getPath());

    }
}

package searchengine.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "lemmas")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id",referencedColumnName = "id", nullable = false)
    private Site site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private int frequency;

    @ManyToMany()
    @JoinTable(name = "index_model",
            joinColumns = {@JoinColumn(name = "lemma_id")},
            inverseJoinColumns = {@JoinColumn(name = "page_id")})
    private List<Page> pages;

    @OneToMany(mappedBy = "lemma")
    private List<IndexModel> indexModels;


}

package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@Setter
@Getter
@Entity
@Table(name = "sites")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private IndexingStatus status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(columnDefinition = "TEXT", name = "last_error")
    private String lastError;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Page> pageList = new ArrayList<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<Lemma> lemmaList = new ArrayList<>();

}

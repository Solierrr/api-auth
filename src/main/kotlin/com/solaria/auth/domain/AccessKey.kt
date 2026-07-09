@Entity
@Table(name = "access_key")
data class AccessKey(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    var code: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    var company: Company,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    var position: Position,

    var active: Boolean = true,

    var maxUses: Int = 1,

    var usedCount: Int = 0,

    var expiresAt: LocalDateTime
)
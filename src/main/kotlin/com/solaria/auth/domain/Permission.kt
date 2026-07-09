@Entity
@Table(name = "permission")
data class Permission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    var description: String?
)
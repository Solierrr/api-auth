@Entity
@Table(name = "position_permission")
data class PositionPermission(

    @EmbeddedId
    var id: PositionPermissionId = PositionPermissionId(),

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("positionId")
    @JoinColumn(name = "position_id")
    var position: Position,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    var permission: Permission
)
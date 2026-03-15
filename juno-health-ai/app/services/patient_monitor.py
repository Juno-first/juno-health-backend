from app.schemas.queue_snapshot import DepartmentQueueSnapshotEvent

CHECK_THRESHOLD_MINUTES = 20

def detect_patients_needing_check(snapshot: DepartmentQueueSnapshotEvent):

    flagged = []

    for e in snapshot.entries:

        if e.visitStatus != "CHECKED_IN":
            continue

        if e.timeInQueueMinutes and e.timeInQueueMinutes >= CHECK_THRESHOLD_MINUTES:
            flagged.append(e)

    return flagged
package com.example.assignmenttracker.data.repo;

import androidx.annotation.NonNull;

/**
 * What the home screen tells the milpa when assignments are finished. Writes are fire-and-forget:
 * a failed credit costs a little farm progress, never the assignment itself.
 */
public interface CompletionLedger {

    /** {@code assignmentId} was marked done at {@code at}. Counts once per assignment, ever. */
    void credit(@NonNull String assignmentId, long at);

    /** The mark done at {@code since} was undone: take back the credit it created, if any. */
    void uncredit(@NonNull String assignmentId, long since);
}

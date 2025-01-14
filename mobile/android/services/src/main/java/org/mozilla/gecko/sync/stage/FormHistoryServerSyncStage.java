/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.stage;

import java.net.URISyntaxException;

import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.middleware.BufferingMiddlewareRepository;
import org.mozilla.gecko.sync.middleware.storage.MemoryBufferStorage;
import org.mozilla.gecko.sync.repositories.ConfigurableServer15Repository;
import org.mozilla.gecko.sync.repositories.NonPersistentRepositoryStateProvider;
import org.mozilla.gecko.sync.repositories.RecordFactory;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.android.FormHistoryRepositorySession;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.domain.VersionConstants;

public class FormHistoryServerSyncStage extends ServerSyncStage {

  // Eventually this kind of sync stage will be data-driven,
  // and all this hard-coding can go away.
  private static final String FORM_HISTORY_SORT = "oldest";
  private static final long FORM_HISTORY_BATCH_LIMIT = 5000;

  @Override
  protected String getCollection() {
    return "forms";
  }

  @Override
  protected String getEngineName() {
    return "forms";
  }

  @Override
  public Integer getStorageVersion() {
    return VersionConstants.FORMS_ENGINE_VERSION;
  }

  /**
   * We're downloading records into a non-persistent buffer for safety, so we can't use a H.W.M.
   * Once this stage is using a persistent buffer, this should change.
   *
   * @return HighWaterMark.Disabled
   */
  @Override
  protected HighWaterMark getAllowedToUseHighWaterMark() {
    return HighWaterMark.Disabled;
  }

  /**
   * Full batching is allowed, because we want all of the records.
   *
   * @return MultipleBatches.Enabled
   */
  @Override
  protected MultipleBatches getAllowedMultipleBatches() {
    return MultipleBatches.Enabled;
  }

  @Override
  protected Repository getRemoteRepository() throws URISyntaxException {
    String collection = getCollection();
    return new ConfigurableServer15Repository(
            collection,
            session.getSyncDeadline(),
            session.config.storageURL(),
            session.getAuthHeaderProvider(),
            session.config.infoCollections,
            session.config.infoConfiguration,
            FORM_HISTORY_BATCH_LIMIT,
            FORM_HISTORY_SORT,
            getAllowedMultipleBatches(),
            getAllowedToUseHighWaterMark(),
            getRepositoryStateProvider(),
            false,
            false
    );
  }

  @Override
  protected Repository getLocalRepository() {
    return new BufferingMiddlewareRepository(
            session.getSyncDeadline(),
            new MemoryBufferStorage(),
            new FormHistoryRepositorySession.FormHistoryRepository()
    );
  }

  public class FormHistoryRecordFactory extends RecordFactory {

    @Override
    public Record createRecord(Record record) {
      FormHistoryRecord r = new FormHistoryRecord();
      r.initFromEnvelope((CryptoRecord) record);
      return r;
    }
  }

  @Override
  protected RecordFactory getRecordFactory() {
    return new FormHistoryRecordFactory();
  }
}

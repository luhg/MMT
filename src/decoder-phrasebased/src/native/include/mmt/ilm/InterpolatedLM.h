//
// Created by Davide Caroselli on 27/07/16.
//

#ifndef ILM_INTERPOLATEDLM_H
#define ILM_INTERPOLATEDLM_H

#include "LM.h"
#include "Options.h"
#include <mmt/IncrementalModel.h>
#include <cstdint>
#include <vector>
#include <string>

using namespace std;

namespace mmt {
    namespace ilm {

        class InterpolatedLM : public LM, public IncrementalModel {
            friend class CachedLM;

        public:
            InterpolatedLM(const string &modelPath, const Options &options = Options());

            ~InterpolatedLM();

            /* LM */

            inline virtual float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                                    const context_t *context,
                                                    HistoryKey **outHistoryKey) const override {
                return ComputeProbability(word, historyKey, context, outHistoryKey, NULL);
            }

            virtual HistoryKey *MakeHistoryKey(const vector <wid_t> &phrase) const override;

            virtual HistoryKey *MakeEmptyHistoryKey() const override;

            virtual bool IsOOV(const context_t *context, const wid_t word) const override;

            virtual void NormalizeContext(context_t *context);

            /* Incremental Model */

            void OnUpdateBatchReceived(const update_batch_t &batch) override;

            virtual unordered_map<channel_t, seqid_t> GetLatestUpdatesIdentifier() override;

        private:
            struct ilm_private;
            ilm_private *self;

            float ComputeProbability(const wid_t word, const HistoryKey *historyKey,
                                     const context_t *context, HistoryKey **outHistoryKey, void *cache) const;
        };

    }
}

#endif //ILM_INTERPOLATEDLM_H

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.plugin.kafka.encoder.json.format;

import io.prestosql.spi.type.SqlTime;
import io.prestosql.spi.type.SqlTimestamp;
import io.prestosql.spi.type.Type;

import static io.prestosql.plugin.kafka.encoder.json.format.util.TimeConversions.scaleEpochMillisToSeconds;
import static io.prestosql.plugin.kafka.encoder.json.format.util.TimeConversions.scalePicosToSeconds;
import static io.prestosql.spi.type.TimeType.TIME_MILLIS;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_MILLIS;

public class SecondsSinceEpochFormatter
        implements JsonDateTimeFormatter
{
    public static boolean isSupportedType(Type type)
    {
        // seconds-since-epoch cannot encode a timezone hence writing TIMESTAMP WITH TIME ZONE
        // is not supported to avoid losing the irrecoverable timezone information after write.
        // TODO allow TIMESTAMP_TZ_MILLIS to be inserted too https://github.com/prestosql/presto/issues/5955
        return type.equals(TIME_MILLIS) ||
                type.equals(TIMESTAMP_MILLIS);
    }

    @Override
    public String formatTime(SqlTime value, int precision)
    {
        return String.valueOf(scalePicosToSeconds(value.getPicos()));
    }

    @Override
    public String formatTimestamp(SqlTimestamp value)
    {
        return String.valueOf(scaleEpochMillisToSeconds(value.getMillis()));
    }
}

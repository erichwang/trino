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
package io.prestosql.plugin.kafka.encoder.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.prestosql.plugin.kafka.encoder.AbstractRowEncoder;
import io.prestosql.plugin.kafka.encoder.EncoderColumnHandle;
import io.prestosql.plugin.kafka.encoder.json.format.DateTimeFormat;
import io.prestosql.plugin.kafka.encoder.json.format.JsonDateTimeFormatter;
import io.prestosql.plugin.kafka.encoder.json.format.UnimplementedJsonDateTimeFormatter;
import io.prestosql.spi.connector.ConnectorSession;
import io.prestosql.spi.type.SqlDate;
import io.prestosql.spi.type.SqlTime;
import io.prestosql.spi.type.SqlTimeWithTimeZone;
import io.prestosql.spi.type.SqlTimestamp;
import io.prestosql.spi.type.SqlTimestampWithTimeZone;
import io.prestosql.spi.type.TimeType;
import io.prestosql.spi.type.Type;
import io.prestosql.spi.type.VarcharType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static io.prestosql.spi.type.BigintType.BIGINT;
import static io.prestosql.spi.type.BooleanType.BOOLEAN;
import static io.prestosql.spi.type.DateType.DATE;
import static io.prestosql.spi.type.DoubleType.DOUBLE;
import static io.prestosql.spi.type.IntegerType.INTEGER;
import static io.prestosql.spi.type.SmallintType.SMALLINT;
import static io.prestosql.spi.type.TimeType.TIME_MILLIS;
import static io.prestosql.spi.type.TimeWithTimeZoneType.TIME_WITH_TIME_ZONE;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP_MILLIS;
import static io.prestosql.spi.type.TimestampWithTimeZoneType.TIMESTAMP_TZ_MILLIS;
import static io.prestosql.spi.type.TinyintType.TINYINT;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

public class JsonRowEncoder
        extends AbstractRowEncoder
{
    private static final Set<Type> SUPPORTED_PRIMITIVE_TYPES = ImmutableSet.of(
            BIGINT, INTEGER, SMALLINT, TINYINT, DOUBLE, BOOLEAN);

    public static final String NAME = "json";

    private final ObjectMapper objectMapper;
    private final ObjectNode node;
    private final List<JsonDateTimeFormatter> dateTimeFormatters;

    JsonRowEncoder(ConnectorSession session, List<EncoderColumnHandle> columnHandles, ObjectMapper objectMapper)
    {
        super(session, columnHandles);

        ImmutableList.Builder<JsonDateTimeFormatter> dateTimeFormatters = ImmutableList.builder();
        for (EncoderColumnHandle columnHandle : this.columnHandles) {
            checkArgument(isSupportedType(columnHandle.getType()), "Unsupported column type '%s' for column '%s'", columnHandle.getType(), columnHandle.getName());

            if (isSupportedTemporalType(columnHandle.getType())) {
                checkArgument(columnHandle.getDataFormat() != null, "Unsupported or no dataFormat '%s' defined for temporal column '%s'", columnHandle.getDataFormat(), columnHandle.getName());
                DateTimeFormat dataFormat = parseDataFormat(columnHandle.getDataFormat(), columnHandle.getName());
                checkArgument(dataFormat.isSupportedType(columnHandle.getType()), "Unsupported column type '%s' for column '%s'", columnHandle.getType(), columnHandle.getName());

                if (dataFormat == DateTimeFormat.CUSTOM_DATE_TIME) {
                    checkArgument(columnHandle.getFormatHint() != null, "No format hint defined for column '%s'", columnHandle.getName());
                }
                else {
                    checkArgument(columnHandle.getFormatHint() == null, "Unexpected format hint '%s' defined for column '%s'", columnHandle.getFormatHint(), columnHandle.getName());
                }

                dateTimeFormatters.add(dataFormat.getFormatter(Optional.ofNullable(columnHandle.getFormatHint())));
            }
            else {
                checkArgument(columnHandle.getFormatHint() == null, "Unexpected format hint '%s' defined for column '%s'", columnHandle.getFormatHint(), columnHandle.getName());
                checkArgument(columnHandle.getDataFormat() == null, "Unexpected data format '%s' defined for column '%s'", columnHandle.getDataFormat(), columnHandle.getName());
                dateTimeFormatters.add(new UnimplementedJsonDateTimeFormatter());
            }
        }

        this.dateTimeFormatters = dateTimeFormatters.build();
        this.objectMapper = requireNonNull(objectMapper, "objectMapper is null");
        this.node = objectMapper.createObjectNode();
    }

    private boolean isSupportedType(Type type)
    {
        return type instanceof VarcharType ||
                SUPPORTED_PRIMITIVE_TYPES.contains(type) ||
                isSupportedTemporalType(type);
    }

    private boolean isSupportedTemporalType(Type type)
    {
        return type.equals(DATE) ||
                type.equals(TIME_MILLIS) ||
                type.equals(TIME_WITH_TIME_ZONE) ||
                type.equals(TIMESTAMP_MILLIS) ||
                type.equals(TIMESTAMP_TZ_MILLIS);
    }

    private DateTimeFormat parseDataFormat(String dataFormat, String columnName)
    {
        try {
            return DateTimeFormat.valueOf(dataFormat.toUpperCase(ENGLISH).replaceAll("-", "_").strip());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(format("Unable to parse data format '%s' for column '%s'", dataFormat, columnName), e);
        }
    }

    private String currentColumnMapping()
    {
        return columnHandles.get(currentColumnIndex).getMapping();
    }

    @Override
    protected void appendNullValue()
    {
        node.putNull(currentColumnMapping());
    }

    @Override
    protected void appendLong(long value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendInt(int value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendShort(short value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendByte(byte value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendDouble(double value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendFloat(float value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendBoolean(boolean value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendString(String value)
    {
        node.put(currentColumnMapping(), value);
    }

    @Override
    protected void appendByteBuffer(ByteBuffer value)
    {
        node.put(currentColumnMapping(), value.array());
    }

    @Override
    protected void appendSqlDate(SqlDate value)
    {
        node.put(currentColumnMapping(), dateTimeFormatters.get(currentColumnIndex).formatDate(value));
    }

    @Override
    protected void appendSqlTime(SqlTime value)
    {
        int precision = ((TimeType) columnHandles.get(currentColumnIndex).getType()).getPrecision();
        node.put(currentColumnMapping(), dateTimeFormatters.get(currentColumnIndex).formatTime(value, precision));
    }

    @Override
    protected void appendSqlTimeWithTimeZone(SqlTimeWithTimeZone value)
    {
        node.put(currentColumnMapping(), dateTimeFormatters.get(currentColumnIndex).formatTimeWithZone(value));
    }

    @Override
    protected void appendSqlTimestamp(SqlTimestamp value)
    {
        node.put(currentColumnMapping(), dateTimeFormatters.get(currentColumnIndex).formatTimestamp(value));
    }

    @Override
    protected void appendSqlTimestampWithTimeZone(SqlTimestampWithTimeZone value)
    {
        node.put(currentColumnMapping(), dateTimeFormatters.get(currentColumnIndex).formatTimestampWithZone(value));
    }

    @Override
    public byte[] toByteArray()
    {
        // make sure entire row has been updated with new values
        checkArgument(currentColumnIndex == columnHandles.size(), format("Missing %d columns", columnHandles.size() - currentColumnIndex + 1));

        try {
            resetColumnIndex(); // reset currentColumnIndex to prepare for next row
            return objectMapper.writeValueAsBytes(node);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.netcloud.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public class DbMetadata {
    @NonNull String dbName;
    @NonNull String connectionString;
}

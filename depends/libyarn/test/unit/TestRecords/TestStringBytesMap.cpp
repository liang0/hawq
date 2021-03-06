/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#include <string>

#include "gtest/gtest.h"
#include "gmock/gmock.h"

#include "records/StringBytesMap.h"

using std::string;
using namespace libyarn;
using namespace testing;

class TestStringBytesMap: public ::testing::Test {
protected:
	StringBytesMap stringMap;
};

TEST_F(TestStringBytesMap, TestGetKey)
{
	string key("key");
	string retKey;

	stringMap.setKey(key);
	retKey = stringMap.getKey();

	EXPECT_EQ(key, retKey);
}

TEST_F(TestStringBytesMap, TestGetValue)
{
	string value("value");
	string retValue;

	stringMap.setValue(value);
	retValue = stringMap.getValue();

	EXPECT_EQ(value, retValue);
}

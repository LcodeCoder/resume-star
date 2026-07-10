#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SQLite -> MySQL 一次性数据迁移脚本
- schema 由 Flyway 负责创建（先启动一次应用让 Flyway 建表，或已存在同名表）
- 本脚本仅搬运数据：对每张 rl_ 表，TRUNCATE 目标表后，按「两库共有列」复制行
- 幂等：可重复执行（每次先 TRUNCATE），失败不写半份

用法：
  MYSQL_HOST=... MYSQL_USER=... MYSQL_PASSWORD=... MYSQL_DB=resume-lcode \
  SQLITE_PATH=./data/resume-lcode.db python3 tools/migrate_sqlite_to_mysql.py
"""
import os
import sqlite3
import sys

try:
    import pymysql
except ImportError:
    sys.exit("需要 pymysql：pip3 install pymysql")

SQLITE_PATH = os.environ.get("SQLITE_PATH", "./data/resume-lcode.db")
MYSQL = dict(
    host=os.environ.get("MYSQL_HOST", "149.88.72.233"),
    port=int(os.environ.get("MYSQL_PORT", "3306")),
    user=os.environ.get("MYSQL_USER", "resume-lcode"),
    password=os.environ["MYSQL_PASSWORD"],  # 必填
    database=os.environ.get("MYSQL_DB", "resume-lcode"),
    charset="utf8mb4",
)


def sqlite_tables(cur):
    cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'rl\\_%' ESCAPE '\\'")
    return [r[0] for r in cur.fetchall()]


def sqlite_columns(cur, table):
    cur.execute(f'PRAGMA table_info("{table}")')
    return [r[1] for r in cur.fetchall()]


def mysql_columns(mcur, table):
    mcur.execute(
        "SELECT column_name FROM information_schema.columns WHERE table_schema=%s AND table_name=%s",
        (MYSQL["database"], table),
    )
    return [r[0] for r in mcur.fetchall()]


def main():
    if not os.path.exists(SQLITE_PATH):
        sys.exit(f"找不到 SQLite 文件：{SQLITE_PATH}")

    sconn = sqlite3.connect(SQLITE_PATH)
    scur = sconn.cursor()
    mconn = pymysql.connect(**MYSQL)
    mcur = mconn.cursor()

    total_rows = 0
    report = []
    try:
        mcur.execute("SET FOREIGN_KEY_CHECKS=0")
        for table in sqlite_tables(scur):
            mcols = mysql_columns(mcur, table)
            if not mcols:
                report.append(f"  跳过 {table}：MySQL 无同名表（未被 Flyway 管理）")
                continue
            scols = sqlite_columns(scur, table)
            cols = [c for c in scols if c in mcols]  # 两库共有列
            if not cols:
                report.append(f"  跳过 {table}：无共有列")
                continue

            scur.execute(f'SELECT {", ".join(f_quote_sqlite(cols))} FROM "{table}"')
            rows = scur.fetchall()

            mcur.execute(f"TRUNCATE TABLE `{table}`")
            if rows:
                placeholders = ", ".join(["%s"] * len(cols))
                collist = ", ".join(f"`{c}`" for c in cols)
                mcur.executemany(
                    f"INSERT INTO `{table}` ({collist}) VALUES ({placeholders})", rows
                )
            total_rows += len(rows)
            report.append(f"  {table}: {len(rows)} 行")
        mcur.execute("SET FOREIGN_KEY_CHECKS=1")
        mconn.commit()
    except Exception as e:
        mconn.rollback()
        raise
    finally:
        scur.close(); sconn.close()
        mcur.close(); mconn.close()

    print("迁移完成，共 %d 行：" % total_rows)
    print("\n".join(report))


def f_quote_sqlite(cols):
    return [f'"{c}"' for c in cols]


if __name__ == "__main__":
    main()

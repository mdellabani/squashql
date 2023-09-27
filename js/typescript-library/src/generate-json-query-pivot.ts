import * as fs from "fs";
import {tableField, tableFields} from "./field";
import { avg } from "./measure";
import { createPivotTableQuery } from "./querier";
import { from } from "./queryBuilder";

export function generateFromQueryPivot() {
  const fields = tableFields(["a", "b"]);

  const q = from("myTable")
          .select(fields,
                  [],
                  [avg("sum", "f1")])
          .build()

  const pivotQuery = createPivotTableQuery(q, {rows: [tableField("a")], columns: [tableField("b")]})
  console.log(JSON.stringify(pivotQuery))
  const data = JSON.stringify(pivotQuery)
  fs.writeFileSync('build-from-query-pivot.json', data)
}

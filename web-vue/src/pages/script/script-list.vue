<template>
  <div class="node-full-content">
    <div ref="filter" class="filter">
      <a-space>
        <a-input v-model="listQuery['%name%']" placeholder="名称" allowClear class="search-input-item" />
        <a-input v-model="listQuery['%description%']" placeholder="描述" class="search-input-item" />
        <a-input v-model="listQuery['%autoExecCron%']" placeholder="定时执行" class="search-input-item" />
        <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
          <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
        </a-tooltip>
        <a-button type="primary" @click="createScript">新建脚本</a-button>

        <a-tooltip>
          <template slot="title">
            <div>脚本模版是存储在服务端中的命令脚本用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

            <div>
              <ul>
                <li>执行时候默认不加载全部环境变量、需要脚本里面自行加载</li>
                <li>命令文件将在 ${数据目录}/script/xxxx.sh、bat 执行</li>
                <li>分发节点是指在编辑完脚本后自动将脚本内容同步节点的脚本,一般用户节点分发功能中的 DSL 模式</li>
              </ul>
            </div>
          </template>
          <a-icon type="question-circle" theme="filled" />
        </a-tooltip>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" @change="changePage" :pagination="this.listQuery.total / this.listQuery.limit > 1 ? (this, pagination) : false" bordered rowKey="id">
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip
        slot="modifyTimeMillis"
        slot-scope="text, record"
        :title="`创建时间：${parseTime(record.createTimeMillis)} ${record.modifyTimeMillis ? '修改时间：' + parseTime(record.modifyTimeMillis) : ''}`"
      >
        <span>{{ parseTime(record.modifyTimeMillis) }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" @click="handleExec(record)">执行</a-button>
          <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editScriptVisible" title="编辑 Script" @ok="handleEditScriptOk" :maskClosable="false" width="80vw">
      <a-form-model ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item v-if="temp.id" label="ScriptId" prop="id">
          <a-input v-model="temp.id" disabled readonly />
        </a-form-model-item>
        <a-form-model-item label="Script 名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称" />
        </a-form-model-item>
        <a-form-model-item label="Script 内容" prop="context">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-model-item>
        <a-form-model-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-model-item>
        <a-form-model-item label="定时执行" prop="autoExecCron">
          <a-auto-complete v-model="temp.autoExecCron" placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）" option-label-prop="value">
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
              </a-select-opt-group>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item label="描述" prop="description">
          <a-input v-model="temp.description" type="textarea" :rows="3" style="resize: none" placeholder="详细描述" />
        </a-form-model-item>
        <a-form-model-item>
          <template slot="label">
            分发节点
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 分发节点是指在编辑完脚本后自动将脚本内容同步节点的脚本中 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select show-search option-filter-prop="children" placeholder="请选择分发到的节点" mode="multiple" v-model="temp.chooseNode">
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <script-console v-if="drawerConsoleVisible" :defArgs="temp.defArgs" :id="temp.id" />
    </a-drawer>
  </div>
</template>
<script>
import { getScriptListAll, editScript, deleteScript } from "@/api/server-script";
import codeEditor from "@/components/codeEditor";
import { getNodeListAll } from "@/api/node";
import ScriptConsole from "@/pages/script/script-console";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY, CRON_DATA_SOURCE } from "@/utils/const";
import { parseTime } from "@/utils/time";
export default {
  components: {
    ScriptConsole,
    codeEditor,
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeList: [],
      editScriptVisible: false,
      drawerTitle: "",
      drawerConsoleVisible: false,
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "描述", dataIndex: "description", ellipsis: true, scopedSlots: { customRender: "description" } },
        { title: "定时执行", dataIndex: "autoExecCron", ellipsis: true, scopedSlots: { customRender: "autoExecCron" } },
        { title: "修改时间", dataIndex: "modifyTimeMillis", sorter: true, width: 170, ellipsis: true, scopedSlots: { customRender: "modifyTimeMillis" } },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "最后操作人", dataIndex: "lastRunUser", ellipsis: true, scopedSlots: { customRender: "lastRunUser" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 260 },
      ],
      rules: {
        name: [{ required: true, message: "Please input Script name", trigger: "blur" }],
        context: [{ required: true, message: "Please input Script context", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return {
        total: this.listQuery.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  mounted() {
    // this.calcTableHeight();

    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getScriptListAll(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    parseTime(v) {
      return parseTime(v);
    },
    // 获取所有节点
    getAllNodeList() {
      getNodeListAll().then((res) => {
        this.nodeList = res.data || [];
      });
    },
    createScript() {
      this.temp = {};
      this.editScriptVisible = true;
      this.getAllNodeList();
    },
    // 修改
    handleEdit(record) {
      this.temp = record;
      //
      // this.temp.;
      this.temp = { ...this.temp, chooseNode: record.nodeIds ? record.nodeIds.split(",") : [] };
      this.editScriptVisible = true;
      this.getAllNodeList();
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      // 检验表单
      this.$refs["editScriptForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        this.temp.nodeIds = this.temp?.chooseNode?.join(",");
        editScript(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.editScriptVisible = false;
            this.loadData();
            this.$refs["editScriptForm"].resetFields();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除脚本么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          // 删除
          deleteScript(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭 console
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
  },
};
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
</style>

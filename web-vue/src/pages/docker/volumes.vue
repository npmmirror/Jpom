<template>
  <a-table :data-source="list" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
    <template slot="title">
      <a-space>
        <a-input v-model="listQuery['name']" @keyup.enter="loadData" placeholder="名称" class="search-input-item" />

        <div>
          悬空
          <a-switch checked-children="是" un-checked-children="否" v-model="listQuery['dangling']" />
        </div>

        <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
      </a-space>
    </template>

    <a-tooltip slot="CreatedAt" slot-scope="text" placement="topLeft" :title="text['CreatedAt']">
      <span>{{ text["CreatedAt"] }}</span>
    </a-tooltip>
    <!-- <a-tooltip slot="size" slot-scope="text, record" placement="topLeft" :title="renderSize(text) + ' ' + renderSize(record.virtualSize)">
      <span>{{ renderSize(text) }}</span>
    </a-tooltip> -->

    <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
      <span>{{ text }}</span>
    </a-tooltip>

    <a-tooltip slot="id" slot-scope="text" :title="text">
      <span> {{ text.split(":")[1].slice(0, 12) }}</span>
    </a-tooltip>
    <template slot="operation" slot-scope="text, record">
      <a-space>
        <!-- <a-tooltip title="停止" v-if="record.state === 'running'">
          <a-button size="small" type="link" @click="doAction(record, 'stop')"><a-icon type="stop" /></a-button>
        </a-tooltip>
        <a-tooltip title="启动" v-else>
          <a-button size="small" type="link" @click="doAction(record, 'start')"> <a-icon type="play-circle" /></a-button>
        </a-tooltip>
        <a-tooltip title="重启">
          <a-button size="small" type="link" :disabled="record.state !== 'running'" @click="doAction(record, 'restart')"><a-icon type="reload" /></a-button>
        </a-tooltip> -->
        <a-tooltip title="删除">
          <a-button size="small" type="link" @click="doAction(record, 'remove')"><a-icon type="delete" /></a-button>
        </a-tooltip>
      </a-space>
    </template>
  </a-table>
</template>
<script>
import { renderSize } from "@/utils/time";
import { dockerVolumesList, dockerVolumesRemove } from "@/api/docker-api";
export default {
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      list: [],
      loading: false,
      listQuery: {
        dangling: false,
      },
      renderSize,
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "挂载点", dataIndex: "mountpoint", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "类型", dataIndex: "driver", ellipsis: true, width: 80, scopedSlots: { customRender: "tooltip" } },
        { title: "创建时间", dataIndex: "rawValues", ellipsis: true, width: 180, scopedSlots: { customRender: "CreatedAt" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 80 },
      ],
      action: {
        remove: {
          msg: "您确定要删除当前卷吗？",
          api: dockerVolumesRemove,
        },
      },
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      //this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.id = this.id;
      dockerVolumesList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    doAction(record, actionKey) {
      const action = this.action[actionKey];
      if (!action) {
        return;
      }
      this.$confirm({
        title: "系统提示",
        content: action.msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: this.id,
            volumeName: record.name,
          };
          action.api(params).then((res) => {
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
  },
};
</script>
